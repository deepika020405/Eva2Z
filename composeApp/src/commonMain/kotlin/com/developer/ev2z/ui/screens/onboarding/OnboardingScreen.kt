package com.developer.ev2z.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private object AppColors {
    val Background     = Color(0xFFF8FAFC)
    val Card           = Color.White
    val Green          = Color(0xFF00A844)
    val GreenSoft      = Color(0xFFE8F7EE)
    val DarkPill       = Color(0xFF0F2A3D)
    val ButtonDark     = Color(0xFF101828)
    val TextPrimary    = Color(0xFF0F172A)
    val TextSecondary  = Color(0xFF475569)
    val CardBorder     = Color(0xFFE2E8F0)
    val SelectedBorder = Color(0xFF00A844)
}

private data class ProtectionPlan(
    val id: String,
    val title: String,
    val subtitle: String,
    val features: List<String>,
    val badgeText: String,
    val isFree: Boolean,
    val ctaText: String
)

private val plans = listOf(
    ProtectionPlan(
        id = "qr",
        title = "QR Tag Protection",
        subtitle = "Get a free QR sticker for your vehicle",
        features = listOf(
            "Anyone can contact you via QR scan",
            "Document vault for RC, Insurance, PUC",
            "Community alerts and safety score",
            "No hardware — works instantly"
        ),
        badgeText = "FREE",
        isFree = true,
        ctaText = "Get Started"
    ),
    ProtectionPlan(
        id = "gps",
        title = "GPS Live Tracking",
        subtitle = "Real-time location and remote control",
        features = listOf(
            "Live GPS tracking 24/7",
            "Remote engine on/off",
            "Geofence alerts",
            "Includes everything from QR plan"
        ),
        badgeText = "FROM ₹4,200",
        isFree = false,
        ctaText = "Choose GPS"
    ),
    ProtectionPlan(
        id = "other",
        title = "Other Services",
        subtitle = "Documents, community & verification tools",
        features = listOf(
            "Document vault for RC, Insurance, PUC",
            "Community alerts and safety updates",
            "Criminal background verification",
            "Shop for accessories and GPS devices"
        ),
        badgeText = "EXPLORE",
        isFree = false,
        ctaText = "Explore Services"
    )
)

@Composable
fun OnboardingScreen(
    onBack: () -> Unit = {},
    onContinue: (String) -> Unit = {}
) {
    var selectedId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = AppColors.Background,
        bottomBar = {
            ContinueBar(
                enabled = selectedId != null,
                onClick = { selectedId?.let(onContinue) }
            )
        }
    ) { inner ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = inner.calculateBottomPadding())
        ) {
            val isWide = maxWidth >= 600.dp
            val hPad = if (isWide) 32.dp else 20.dp
            val contentMax = if (isWide) 560.dp else maxWidth

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = contentMax)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = hPad)
                ) {
                    Spacer(Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppColors.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "How do you want to protect your vehicle?",
                        color = AppColors.TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 26.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Choose one. You can upgrade anytime.",
                        color = AppColors.TextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(24.dp))

                    plans.forEach { plan ->
                        PlanCard(
                            plan = plan,
                            selected = selectedId == plan.id,
                            onClick = { selectedId = plan.id }
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: ProtectionPlan,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Card)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) AppColors.SelectedBorder else AppColors.CardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        if (plan.isFree) FreeBadge() else PriceBadge(plan.badgeText)

        Spacer(Modifier.height(14.dp))

        Text(
            text = plan.title,
            color = AppColors.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = plan.subtitle,
            color = AppColors.TextSecondary,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(14.dp))

        plan.features.forEach { feature ->
            FeatureRow(feature)
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onClick)
        ) {
            Text(
                text = plan.ctaText,
                color = AppColors.Green,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = AppColors.Green,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun FreeBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(AppColors.GreenSoft)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = "FREE", color = AppColors.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PriceBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(AppColors.DarkPill)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = AppColors.Green,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(text = text, color = AppColors.TextPrimary, fontSize = 12.sp)
    }
}

@Composable
private fun ContinueBar(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Background)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (enabled) AppColors.ButtonDark
                    else AppColors.ButtonDark.copy(alpha = 0.5f)
                )
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Continue",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}