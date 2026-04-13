package com.developer.ev2z.ui.screens.homeScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GreenPrimary = Color(0xFF00C853)
private val GreenDark    = Color(0xFF00A844)

@Composable
fun AllFeaturesScreen(
    features: List<QuickAction>,
    onBack: () -> Unit
) {
    val palette = LocalHomeTheme.current.palette
    val PageBg       = palette.pageBg
    val TextPrimary  = palette.textPrimary
    val TextSecondary = palette.textSecondary
    val TextMuted    = palette.textMuted
    val BorderSoft   = palette.borderSoft
    val SoftShadow   = palette.softShadow
    val SurfaceColor = if (palette.isDark) palette.cardBg else Color.White
    val SurfaceTopGradEnd = if (palette.isDark) palette.cardBg else Color(0xFFF8FAFC)
    val backBtnBg = if (palette.isDark) Color(0xFF0F2950) else Color(0xFFF1F5F9)
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(PageBg)) {
        val isTablet = maxWidth >= 600.dp
        val cols = if (isTablet) 3 else 2
        val hPad: Dp = if (isTablet) 24.dp else 16.dp
        val gridSpacing: Dp = if (isTablet) 14.dp else 12.dp

        Scaffold(containerColor = PageBg) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // ─── Top bar ──────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 4.dp,
                            ambientColor = SoftShadow,
                            spotColor = SoftShadow
                        )
                        .background(
                            Brush.linearGradient(
                                listOf(SurfaceColor, SurfaceTopGradEnd)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = hPad, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(backBtnBg)
                            .border(0.5.dp, BorderSoft, CircleShape)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Vehicle Features",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.3).sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${features.size} tools to manage your vehicle",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    // Stat pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(GreenPrimary.copy(0.12f))
                            .border(0.5.dp, GreenPrimary.copy(0.3f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 11.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "ALL",
                            color = GreenDark,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // ─── Grid ─────────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = hPad, vertical = 16.dp)
                ) {
                    features.chunked(cols).forEach { rowFeatures ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(gridSpacing)
                        ) {
                            rowFeatures.forEach { feat ->
                                FeatureCard(
                                    feat = feat,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(cols - rowFeatures.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(gridSpacing))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "More features coming soon \u2728",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 24.dp)
                    )
                }
            }
        }
    }
}