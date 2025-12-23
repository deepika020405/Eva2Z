package com.developer.ev2z.ui.screens.Profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAppScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "About EVA2Z Connect",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome
            SectionHeader("🚗 Welcome to EVA2Z Connect!")
            SectionText(
                "EVA2Z Connect is your all-in-one GPS tracking and vehicle security app — designed to give you " +
                        "total control, real-time safety, and complete peace of mind.\n\n" +
                        "Whether you're a personal vehicle owner, fleet manager, or someone who wants to protect loved ones " +
                        "and valuable assets, EVA2Z Connect gives you everything in one powerful app."
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Features
            SectionHeader("🚀 What You Can Do")
            BulletPoint("📍 Live GPS Tracking", "See your vehicle location in real time.")
            BulletPoint("🔐 Remote Engine Control", "Instantly secure your vehicle from anywhere.")
            BulletPoint("🛣️ Route History", "Check every route throughout the day.")
            BulletPoint("📊 Daily Analytics", "Understand trips, mileage, and usage.")
            BulletPoint("🔔 Smart Alerts", "Ignition, speeding, battery, geofence & more.")
            BulletPoint("🚘 Multi-Vehicle", "Manage car, bike, truck, or fleet.")
            BulletPoint("🛒 In-App Shop", "Buy GPS devices directly.")
            BulletPoint("👤 Profile & Preferences", "Manage settings easily.")
            BulletPoint("🎨 Beautiful UI", "Modern & effortless navigation.")

            Spacer(modifier = Modifier.height(8.dp))

            // Products
            SectionHeader("🛍️ Products Available")
            BulletPoint("🔋 GPS Tracker", "Wireless")
            BulletPoint("🔧 GPS Tracker", "Wired (Car/Bike)")
            BulletPoint("🧒 Kids Tracker", "Keep children safe")
            BulletPoint("🐶 Pet Tracker", "Never lose your pet")
            BulletPoint("🧓 Elderly Tracker", "Care for loved ones")
            BulletPoint("📄 Document Tracker", "Smart Card Tag")

            Spacer(modifier = Modifier.height(8.dp))

            // Why Us
            SectionHeader("💡 Why EVA2Z Stands Out")
            SimpleList(
                listOf(
                    "Faster & accurate tracking",
                    "Reliable engine lock/unlock",
                    "Best safety alerts",
                    "Trusted by 1,000+ users",
                    "Personal & fleet management"
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Company
            SectionHeader("🏢 About the Company")
            SectionText(
                "Evatoz Solutions Private Limited delivers innovative electric mobility and GPS tracking solutions."
            )

            SectionSubHeader("🎯 Our Mission")
            SectionText(
                "To empower users with real-time tracking, smart alerts, and seamless mobile experience."
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Contact
            SectionHeader("📞 Contact & Support")
            ContactInfo("Address:", "B-46, Sector 57, Noida, UP 201301")
            ContactInfo("Phone:", "+91 9266884678")
            ContactInfo("Email:", "info@eva2z.com")
            ContactInfo("Website:", "https://eva2z.com")

            Spacer(modifier = Modifier.height(8.dp))

            // CTA
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⏳ Don't Wait — Protect What Matters!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Every minute without tracking is a risk.",
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🚗 Track Smarter. Live Safer. Stay in Control.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF333333)
    )
}

@Composable
private fun SectionSubHeader(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF4CAF50)
    )
}

@Composable
private fun SectionText(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = Color(0xFF666666),
        lineHeight = 20.sp
    )
}

@Composable
private fun BulletPoint(title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "•",
            fontSize = 14.sp,
            color = Color(0xFF4CAF50),
            modifier = Modifier.padding(top = 2.dp)
        )
        Column {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(title)
                    }
                },
                fontSize = 14.sp,
                color = Color(0xFF333333)
            )
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color(0xFF666666),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun SimpleList(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "•", fontSize = 14.sp, color = Color(0xFF4CAF50))
                Text(text = item, fontSize = 14.sp, color = Color(0xFF666666), lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun ContactInfo(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color(0xFF666666)
        )
    }
}