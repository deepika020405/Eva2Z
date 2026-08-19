package com.developer.ev2z.ui.screens.Profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.theme.AppPageBg


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAppScreen(
    onBackClick: () -> Unit
) {

    PlatformBackHandler(enabled = true) {
        onBackClick()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "About EVA2Z Connect",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            null,
                            tint = Color(0xFF333333)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppPageBg
                )
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val isCompact = screenHeight < 700.dp

            val horizontalPad = (screenWidth * 0.04f).coerceIn(12.dp, 20.dp)
            val cardCorner = 18.dp
            val cardSpacing = if (isCompact) 12.dp else 16.dp
            val sectionTitleSize = if (isCompact) 16.sp else 17.sp
            val bodyTextSize = if (isCompact) 13.sp else 14.sp
            val bodyLineHeight = (bodyTextSize.value * 1.5f).sp
            val featureTitleSize = if (isCompact) 13.sp else 14.sp
            val featureDescSize = if (isCompact) 12.sp else 13.sp
            val cardInnerPad = if (isCompact) 14.dp else 18.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppPageBg)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPad, vertical = horizontalPad),
                verticalArrangement = Arrangement.spacedBy(cardSpacing)
            ) {
                // Welcome Card
                InfoCard(cardCorner, cardInnerPad) {
                    SectionTitle("Welcome to EVA2Z Connect", sectionTitleSize)
                    SectionText(
                        "EVA2Z Connect is an advanced GPS tracking and vehicle security platform designed to deliver real-time monitoring, safety, and complete control.\n\n" +
                                "Ideal for individuals, families, and fleet operators looking for reliability and peace of mind.",
                        bodyTextSize,
                        bodyLineHeight
                    )
                }

                // Features Card
                InfoCard(cardCorner, cardInnerPad) {
                    SectionTitle("Key Features", sectionTitleSize)
                    FeatureItem("Live GPS Tracking", "Real-time vehicle location", featureTitleSize, featureDescSize)
                    FeatureItem("Remote Engine Control", "Secure vehicle instantly", featureTitleSize, featureDescSize)
                    FeatureItem("Route History", "Complete travel logs", featureTitleSize, featureDescSize)
                    FeatureItem("Daily Analytics", "Trips, mileage & insights", featureTitleSize, featureDescSize)
                    FeatureItem("Smart Alerts", "Ignition, speed & geofence", featureTitleSize, featureDescSize)
                    FeatureItem("Multi-Vehicle Support", "Cars, bikes & fleets", featureTitleSize, featureDescSize)
                }

                // Products Card
                InfoCard(cardCorner, cardInnerPad) {
                    SectionTitle("Available Products", sectionTitleSize)
                    SimpleItem("Wireless GPS Tracker", bodyTextSize)
                    SimpleItem("Wired GPS Tracker (Car / Bike)", bodyTextSize)
                    SimpleItem("Kids & Pet Tracker", bodyTextSize)
                    SimpleItem("Elderly Safety Tracker", bodyTextSize)
                    SimpleItem("Smart Document Tracker", bodyTextSize)
                }

                // Why Choose Card
                InfoCard(cardCorner, cardInnerPad) {
                    SectionTitle("Why Choose EVA2Z", sectionTitleSize)
                    SimpleItem("Accurate & fast tracking", bodyTextSize)
                    SimpleItem("Reliable engine control", bodyTextSize)
                    SimpleItem("Advanced safety alerts", bodyTextSize)
                    SimpleItem("Trusted by thousands of users", bodyTextSize)
                }

                // Company Card
                InfoCard(cardCorner, cardInnerPad) {
                    SectionTitle("Company Information", sectionTitleSize)
                    SectionText(
                        "Evatoz Solutions Private Limited specializes in electric mobility and intelligent GPS tracking solutions.",
                        bodyTextSize,
                        bodyLineHeight
                    )
                    Spacer(Modifier.height(8.dp))
                    SectionSubtitle("Our Mission", if (isCompact) 14.sp else 15.sp)
                    SectionText(
                        "To empower users with intelligent tracking, real-time alerts, and seamless digital experience.",
                        bodyTextSize,
                        bodyLineHeight
                    )
                }

                // Contact Card
                InfoCard(cardCorner, cardInnerPad) {
                    SectionTitle("Contact & Support", sectionTitleSize)
                    ContactRow("Address", "B-46, Sector 57, Noida, UP 201301", featureTitleSize, featureDescSize)
                    ContactRow("Phone", "+91 9266884678", featureTitleSize, featureDescSize)
                    ContactRow("Email", "info@eva2z.com", featureTitleSize, featureDescSize)
                    ContactRow("Website", "www.eva2z.com", featureTitleSize, featureDescSize)
                }

                // CTA Card
                HighlightCard(isCompact)

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun InfoCard(
    cornerRadius: androidx.compose.ui.unit.Dp = 18.dp,
    innerPadding: androidx.compose.ui.unit.Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun SectionTitle(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit = 17.sp
) {
    Text(
        text,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF222222)
    )
}

@Composable
private fun SectionSubtitle(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit = 15.sp
) {
    Text(
        text,
        fontSize = fontSize,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF6A9F50)
    )
}

@Composable
private fun SectionText(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    lineHeight: androidx.compose.ui.unit.TextUnit = 20.sp
) {
    Text(
        text,
        fontSize = fontSize,
        lineHeight = lineHeight,
        color = Color(0xFF777777)
    )
}

@Composable
private fun FeatureItem(
    title: String,
    desc: String,
    titleSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    descSize: androidx.compose.ui.unit.TextUnit = 13.sp
) {
    Column {
        Text(
            title,
            fontWeight = FontWeight.Medium,
            fontSize = titleSize,
            color = Color(0xFF333333)
        )
        Text(
            desc,
            fontSize = descSize,
            color = Color(0xFF999999)
        )
    }
}

@Composable
private fun SimpleItem(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(Color(0xFF6A9F50), RoundedCornerShape(3.dp))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            fontSize = fontSize,
            color = Color(0xFF555555)
        )
    }
}

@Composable
private fun ContactRow(
    label: String,
    value: String,
    labelSize: androidx.compose.ui.unit.TextUnit = 13.sp,
    valueSize: androidx.compose.ui.unit.TextUnit = 13.sp
) {
    Column {
        Text(
            label,
            fontWeight = FontWeight.Medium,
            fontSize = labelSize,
            color = Color(0xFF333333)
        )
        Text(
            value,
            fontSize = valueSize,
            color = Color(0xFF888888)
        )
    }
}

@Composable
private fun HighlightCard(isCompact: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF6A9F50), Color(0xFF8BC34A))
                    )
                )
                .padding(if (isCompact) 16.dp else 20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Protect What Matters Most",
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isCompact) 15.sp else 17.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Track smarter. Stay safer. Stay in control.",
                    fontSize = if (isCompact) 13.sp else 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}